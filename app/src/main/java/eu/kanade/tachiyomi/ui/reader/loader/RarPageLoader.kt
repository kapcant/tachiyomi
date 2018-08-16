package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.ImageUtil
import junrar.Archive
import junrar.rarfile.FileHeader
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import rx.Observable
import java.io.File

class RarPageLoader(file: File) : PageLoader() {

    private val archive = Archive(file)

    override fun recycle() {
        super.recycle()
        archive.close()
    }

    override fun getPages(): Observable<List<ReaderPage>> {
        val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()

        return archive.fileHeaders
            .filter { !it.isDirectory && ImageUtil.isImage(it.fileNameString) { archive.getInputStream(it) } }
            .sortedWith(Comparator<FileHeader> { f1, f2 -> comparator.compare(f1.fileNameString, f2.fileNameString) })
            .mapIndexed { i, header ->
                val streamFn = { archive.getInputStream(header) }
                ReaderPage(i).apply {
                    stream = streamFn
                    status = Page.READY
                }
            }
            .let { Observable.just(it) }
    }

    override fun getPage(page: ReaderPage): Observable<Int> {
        return Observable.just(if (isRecycled) {
            Page.ERROR
        } else {
            Page.READY
        })
    }

}
