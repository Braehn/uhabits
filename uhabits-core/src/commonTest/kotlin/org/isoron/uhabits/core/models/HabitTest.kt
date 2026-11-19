/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitTest : BaseUnitTest() {

    @BeforeTest
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun testUuidGeneration() {
        val uuid1 = modelFactory.buildHabit().uuid!!
        val uuid2 = modelFactory.buildHabit().uuid!!
        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun test_copyAttributes() {
        val model = modelFactory.buildHabit()
        model.isArchived = true
        model.color = PaletteColor(0)
        model.frequency = Frequency(10, 20)
        model.reminder = Reminder(8, 30, WeekdayList(1))
        val habit = modelFactory.buildHabit()
        habit.copyFrom(model)
        assertEquals(habit.isArchived, model.isArchived)
        assertEquals(model.isArchived, habit.isArchived)
        assertEquals(model.color, habit.color)
        assertEquals(model.frequency, habit.frequency)
        assertEquals(model.reminder, habit.reminder)
    }

    @Test
    fun test_hasReminder() {
        val h = modelFactory.buildHabit()
        assertEquals(false, h.hasReminder())
        h.reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
        assertEquals(true, h.hasReminder())
    }

    @Test
    fun test_isCompleted() {
        val h = modelFactory.buildHabit()
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), Entry.YES_MANUAL))
        h.recompute()
        assertTrue(h.isCompletedToday())
    }

    @Test
    fun test_isEntered() {
        val h = modelFactory.buildHabit()
        assertFalse(h.isEnteredToday())
        h.originalEntries.add(Entry(getToday(), Entry.NO))
        h.recompute()
        assertTrue(h.isEnteredToday())
    }

    @Test
    fun test_isCompleted_numerical() {
        val h = modelFactory.buildHabit()
        h.type = HabitType.NUMERICAL
        h.targetType = NumericalHabitType.AT_LEAST
        h.targetValue = 100.0
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 200000))
        h.recompute()
        assertTrue(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 100000))
        h.recompute()
        assertTrue(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 50000))
        h.recompute()
        assertFalse(h.isCompletedToday())
        h.targetType = NumericalHabitType.AT_MOST
        h.originalEntries.add(Entry(getToday(), 200000))
        h.recompute()
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 100000))
        h.recompute()
        assertTrue(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 50000))
        h.recompute()
        assertTrue(h.isCompletedToday())
    }

    @Test
    fun test_daysSinceLastDone_neverDone() {
        val today = LocalDate(2026, 11, 19)
        val h = modelFactory.buildHabit()
        assertNull(h.daysSinceLastDone(today))
        h.originalEntries.add(Entry(today.minus(1), Entry.NO))
        h.originalEntries.add(Entry(today.minus(2), Entry.SKIP))
        h.recompute()
        assertNull(h.daysSinceLastDone(today))
    }

    @Test
    fun test_daysSinceLastDone_yesNo() {
        val today = LocalDate(2026, 11, 19)
        val h = modelFactory.buildHabit()
        h.originalEntries.add(Entry(today.minus(12), Entry.YES_MANUAL))
        h.originalEntries.add(Entry(today.minus(5), Entry.YES_MANUAL))
        h.originalEntries.add(Entry(today.minus(2), Entry.NO))
        h.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
        h.recompute()
        assertEquals(5, h.daysSinceLastDone(today))

        h.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        h.recompute()
        assertEquals(0, h.daysSinceLastDone(today))
        assertEquals(1, h.daysSinceLastDone(today.plus(1)))
    }

    @Test
    fun test_daysSinceLastDone_ignoresLaterEntries() {
        val today = LocalDate(2026, 11, 19)
        val h = modelFactory.buildHabit()
        h.originalEntries.add(Entry(today.minus(3), Entry.YES_MANUAL))
        h.originalEntries.add(Entry(today.plus(2), Entry.YES_MANUAL))
        h.recompute()
        assertEquals(3, h.daysSinceLastDone(today))
    }

    @Test
    fun test_daysSinceLastDone_ignoresAutomaticCheckmarks() {
        val today = LocalDate(2026, 11, 19)
        val h = modelFactory.buildHabit()
        h.frequency = Frequency(1, 7)
        h.originalEntries.add(Entry(today.minus(6), Entry.YES_MANUAL))
        h.originalEntries.add(Entry(today.minus(1), Entry.YES_AUTO))
        h.recompute()
        assertEquals(6, h.daysSinceLastDone(today))
    }

    @Test
    fun test_daysSinceLastDone_numerical() {
        val today = LocalDate(2026, 11, 19)
        val h = modelFactory.buildHabit()
        h.type = HabitType.NUMERICAL
        h.targetType = NumericalHabitType.AT_LEAST
        h.targetValue = 2.0
        h.originalEntries.add(Entry(today.minus(4), 1500))
        h.originalEntries.add(Entry(today.minus(2), Entry.SKIP))
        h.originalEntries.add(Entry(today.minus(1), 0))
        h.recompute()
        assertEquals(4, h.daysSinceLastDone(today))
    }

    @Test
    fun testURI() {
        assertTrue(habitList.isEmpty)
        val h = modelFactory.buildHabit()
        habitList.add(h)
        assertEquals(0L, h.id)
        assertEquals("content://org.isoron.uhabits/habit/0", h.uriString)
    }
}
