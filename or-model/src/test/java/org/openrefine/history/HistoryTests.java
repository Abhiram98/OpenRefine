/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.model.GridState;

public class HistoryTests {

    GridState initialState;
    GridState intermediateState;
    GridState finalState;

    long firstChangeId = 1234L;
    long secondChangeId = 5678L;

    Change firstChange;
    Change secondChange;

    HistoryEntry firstEntry;
    HistoryEntry secondEntry;

    List<HistoryEntry> entries;

    @BeforeMethod
    public void setUp() {
        initialState = mock(GridState.class);
        intermediateState = mock(GridState.class);
        finalState = mock(GridState.class);
        firstChange = mock(Change.class);
        secondChange = mock(Change.class);
        firstEntry = mock(HistoryEntry.class);
        secondEntry = mock(HistoryEntry.class);

        when(firstChange.apply(initialState)).thenReturn(intermediateState);
        when(secondChange.apply(intermediateState)).thenReturn(finalState);

        when(firstEntry.getId()).thenReturn(firstChangeId);
        when(secondEntry.getId()).thenReturn(secondChangeId);

        when(firstEntry.getChange()).thenReturn(firstChange);
        when(secondEntry.getChange()).thenReturn(secondChange);

        entries = Arrays.asList(firstEntry, secondEntry);
    }

    @Test
    public void testConstruct() {

        History history = new History(initialState, entries, 1);

        Assert.assertEquals(history.getPosition(), 1);
        Assert.assertEquals(history.getCurrentGridState(), intermediateState);
        Assert.assertEquals(history.getEntries(), entries);

        history.undoRedo(secondChangeId);

        Assert.assertEquals(history.getPosition(), 2);
        Assert.assertEquals(history.getCurrentGridState(), finalState);
        Assert.assertEquals(history.getEntries(), entries);

        history.undoRedo(0);

        Assert.assertEquals(history.getPosition(), 0);
        Assert.assertEquals(history.getCurrentGridState(), initialState);
        Assert.assertEquals(history.getEntries(), entries);

        // All changes were called only once
        verify(firstChange, times(1)).apply(initialState);
        verify(secondChange, times(1)).apply(intermediateState);
    }

    @Test
    public void testSaveAndLoad() {

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnknownChangeId() {
        History history = new History(initialState, entries, 1);

        history.undoRedo(34782L);
    }

}