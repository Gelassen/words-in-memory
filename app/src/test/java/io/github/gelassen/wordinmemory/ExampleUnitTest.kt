package io.github.gelassen.wordinmemory

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testShallowCopyInKotlin_onCleanUpOriginArray_copiedItemRemainTheSame() {
        val dataset = mutableListOf(
            TestItem("Jane", 12),
            TestItem("Kein", 42),
            TestItem("MkLee", 7)
        )
        val deepCopyDataset = buildList { addAll(dataset) }

        dataset.clear()

        val originDatasetSize = 3
        assertEquals(originDatasetSize, deepCopyDataset.size)
    }

    @Test
    fun testShallowCopyInKotlin_onChangeItemInOriginArray_copiedItemsHaveBeenChanged() {
        val janeRecord = TestItem("Jane", 12)
        val keinRecord = TestItem("Kein", 42)
        val mcLeeRecord = TestItem("McLee", 7)
        val dataset = mutableListOf(janeRecord, keinRecord, mcLeeRecord)
        val deepCopyDataset = buildList { addAll(dataset) }

        val modifiedOriginJaneRecord = dataset.get(0)
        modifiedOriginJaneRecord.name = "Bear"
        dataset.set(0, modifiedOriginJaneRecord)
        dataset.set(1, TestItem("Tiger", 24))
        dataset.set(2, TestItem("Lion", 0))

        val originDatasetSize = 3
        assertEquals(originDatasetSize, dataset.size)
        assertEquals(janeRecord, deepCopyDataset.get(0))
        assertNotEquals("Jane", deepCopyDataset.get(0).name)
    }

    @Test
    fun testDeepCopyInKotlinForDataClass_onChangeItemInOriginObject_copiedObjectRemainTheSame() {
        val janeRecord = TestItemDataClass("Jane", 12)//TestItem("Jane", 12)
        val deepCopyJaneRecord = mutableListOf(janeRecord).map { it.copy() }

        janeRecord.name = "Bear"

        assertEquals("Bear", janeRecord.name)
        assertNotEquals("Bear", deepCopyJaneRecord.get(0).name)
    }

    @Test
    fun testDeepCopyInKotlin_onChangeItemInOriginObject_copiedObjectRemainTheSame() {
        val janeRecord = TestItem("Jane", 12)
        val deepCopyJaneRecord = janeRecord.deepCopy()

        janeRecord.name = "Bear"

        assertEquals("Bear", janeRecord.name)
        assertNotEquals("Bear", deepCopyJaneRecord.name)
    }

    @Test
    fun testShallowCopyOfDataClass_onChangeRecord_copiedRecordChangedToo() {
        val subj = TestItemDataClass("Jane", 12)
        val shallowCopySubj = subj.copy()

        subj.name = "Bear"

        assertEquals("Bear", subj.name)
        assertEquals("Bear", shallowCopySubj.name) // FIXME crash here, it behaves like deep copy
    }


    private data class TestItemDataClass (
        var name: String, var count: Int
    )
    private class TestItem {
        var name: String
        var count: Int

        constructor(name: String, count: Int) {
            this.name = name
            this.count = count
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestItem

            if (name != other.name) return false
            if (count != other.count) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + count
            return result
        }

        fun deepCopy(): TestItem {
            return TestItem(
                name =  this.name,
                count = Integer.valueOf(this.count)
            )
        }

    }
}