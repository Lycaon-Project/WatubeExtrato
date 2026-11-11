/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * InfoItemsCollector.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.exceptions.FoundAdException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.util.Collections

/**
 * Create a new collector
 * @param serviceId the service id
 * @param comparator the sorting function of the items, or `null` to keep them in the order
 * they were added
 */
public abstract class InfoItemsCollector<I : InfoItem, E : InfoItemExtractor> @JvmOverloads constructor(
    /** The service id */
    public val serviceId: Int,
    private val comparator: Comparator<I>? = null,
) : Collector<I, E> {

    private val itemList = ArrayList<I>()
    private val errorList = ArrayList<Throwable>()

    override val items: List<I>
        get() {
            if (comparator != null) {
                itemList.sortWith(comparator)
            }
            return Collections.unmodifiableList(itemList)
        }

    override val errors: List<Throwable>
        get() = Collections.unmodifiableList(errorList)

    override fun reset() {
        itemList.clear()
        errorList.clear()
    }

    /**
     * Add an error
     * @param error the error
     */
    protected fun addError(error: Exception) {
        errorList.add(error)
    }

    /**
     * Add an item
     * @param item the item
     */
    protected fun addItem(item: I) {
        itemList.add(item)
    }

    override fun commit(extractor: E) {
        try {
            addItem(extract(extractor))
        } catch (_: FoundAdException) {
            // found an ad. Maybe a debug line could be placed here
        } catch (e: ParsingException) {
            addError(e)
        }
    }
}
