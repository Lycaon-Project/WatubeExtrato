package org.schabi.newpipe.extractor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.NewPipe.getServiceByUrl
import org.schabi.newpipe.extractor.ServiceList.SoundCloud
import org.schabi.newpipe.extractor.ServiceList.YouTube

class GeneralServiceTest {
    @Test
    fun getAllServicesTest() {
        assertEquals(NewPipe.getServices().size, ServiceList.all().size)
    }

    @Test
    fun testAllServicesHaveDifferentId() {
        val servicesId = HashSet<Int>()
        for (streamingService in NewPipe.getServices()) {
            val errorMsg = "There are services with the same id = ${streamingService.serviceId}" +
                " (current service > ${streamingService.serviceInfo.name})"

            assertTrue(servicesId.add(streamingService.serviceId), errorMsg)
        }
    }

    @Test
    fun getServiceWithId() {
        assertEquals(YouTube, NewPipe.getService(YouTube.serviceId))
    }

    @Test
    fun getServiceWithUrl() {
        assertEquals(YouTube, getServiceByUrl("https://www.youtube.com/watch?v=_r6CgaFNAGg"))
        assertEquals(YouTube, getServiceByUrl("https://www.youtube.com/channel/UCi2bIyFtz-JdI-ou8kaqsqg"))
        assertEquals(
            YouTube,
            getServiceByUrl("https://www.youtube.com/playlist?list=PLRqwX-V7Uu6ZiZxtDDRCi6uhfTH4FilpH"),
        )
        assertEquals(
            YouTube,
            getServiceByUrl(
                "https://www.google.it/url?sa=t&rct=j&q=&esrc=s&cd=&cad=rja&uact=8" +
                    "&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DHu80uDzh8RY&source=video",
            ),
        )

        assertEquals(SoundCloud, getServiceByUrl("https://soundcloud.com/pegboardnerds"))
        assertEquals(
            SoundCloud,
            getServiceByUrl(
                "https://www.google.com/url?sa=t&url=https%3A%2F%2Fsoundcloud.com%2Fciaoproduction" +
                    "&rct=j&q=&esrc=s&source=web&cd=",
            ),
        )
    }
}
