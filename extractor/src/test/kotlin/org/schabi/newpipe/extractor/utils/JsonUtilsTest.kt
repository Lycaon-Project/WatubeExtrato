package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException

class JsonUtilsTest {

    @Test
    @Throws(JsonParserException::class, ParsingException::class)
    fun testGetValueFlat() {
        val obj = JsonParser.`object`().from(
            "{\"name\":\"John\",\"age\":30,\"cars\":{\"car1\":\"Ford\",\"car2\":\"BMW\",\"car3\":\"Fiat\"}}",
        )
        assertTrue("John" == JsonUtils.getValue(obj, "name"))
    }

    @Test
    @Throws(JsonParserException::class, ParsingException::class)
    fun testGetValueNested() {
        val obj = JsonParser.`object`().from(
            "{\"name\":\"John\",\"age\":30,\"cars\":{\"car1\":\"Ford\",\"car2\":\"BMW\",\"car3\":\"Fiat\"}}",
        )
        assertTrue("BMW" == JsonUtils.getValue(obj, "cars.car2"))
    }

    @Test
    @Throws(JsonParserException::class, ParsingException::class)
    fun testGetArray() {
        val obj = JsonParser.`object`().from(
            "{\"id\":\"0001\",\"type\":\"donut\",\"name\":\"Cake\",\"ppu\":0.55," +
                "\"batters\":{\"batter\":[{\"id\":\"1001\",\"type\":\"Regular\"}," +
                "{\"id\":\"1002\",\"type\":\"Chocolate\"},{\"id\":\"1003\",\"type\":\"Blueberry\"}," +
                "{\"id\":\"1004\",\"type\":\"Devil's Food\"}]}," +
                "\"topping\":[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"}," +
                "{\"id\":\"5005\",\"type\":\"Sugar\"},{\"id\":\"5007\",\"type\":\"Powdered Sugar\"}," +
                "{\"id\":\"5006\",\"type\":\"Chocolate with Sprinkles\"}," +
                "{\"id\":\"5003\",\"type\":\"Chocolate\"},{\"id\":\"5004\",\"type\":\"Maple\"}]}",
        )
        val arr = JsonUtils.getValue(obj, "batters.batter") as JsonArray
        assertTrue(arr.isNotEmpty())
    }
}
