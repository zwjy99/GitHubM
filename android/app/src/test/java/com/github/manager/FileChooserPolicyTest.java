package com.github.manager;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class FileChooserPolicyTest {
    @Test public void apkGoesToDocumentsWithoutCamera() {
        assertFalse(FileChooserPolicy.INSTANCE.needsCamera(Arrays.asList(".apk", "application/vnd.android.package-archive")));
        assertEquals(Collections.singletonList("application/vnd.android.package-archive"), FileChooserPolicy.INSTANCE.mimeTypes(Arrays.asList(".apk,application/vnd.android.package-archive")));
    }
    @Test public void jsonIsAFileNotAPhoto() {
        assertFalse(FileChooserPolicy.INSTANCE.needsCamera(Collections.singletonList("application/json")));
        assertEquals(Collections.singletonList("application/json"), FileChooserPolicy.INSTANCE.mimeTypes(Collections.singletonList(".json")));
    }
    @Test public void unspecifiedAcceptDoesNotRequestCameraPermission() {
        assertFalse(FileChooserPolicy.INSTANCE.needsCamera(Collections.emptyList()));
        assertFalse(FileChooserPolicy.INSTANCE.needsCamera(Collections.singletonList("*/*")));
        assertEquals(Collections.singletonList("*/*"), FileChooserPolicy.INSTANCE.mimeTypes(Collections.emptyList()));
    }
    @Test public void imageInputStillOffersCamera() {
        assertTrue(FileChooserPolicy.INSTANCE.needsCamera(Collections.singletonList("image/*")));
        assertTrue(FileChooserPolicy.INSTANCE.needsCamera(Collections.singletonList(".png")));
    }
}
