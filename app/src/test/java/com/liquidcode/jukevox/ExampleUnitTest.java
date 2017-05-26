package com.liquidcode.jukevox;

import com.liquidcode.jukevox.musicobjects.Album;
import com.liquidcode.jukevox.musicobjects.Song;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test_music() {
        // test stuff that i have to come back and finish.
        // should be doing unit tests
        Album a = new Album("My Album");
        a.songList.add(0, new Song("Test Song", 0, 1000));
        assertEquals(true, a.songList.size() == 1);
    }


}