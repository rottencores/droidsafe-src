package java.lang;

// Droidsafe Imports
import droidsafe.runtime.*;
import droidsafe.helpers.*;
import droidsafe.annotations.*;

public class NoSuchMethodException extends Exception {
@DSGeneratedField(tool_name = "Doppelganger", tool_version = "2.0", generated_on = "2013-12-30 12:56:31.840 -0500", hash_original_field = "74E7CB73F6E22B09EB59317D410CBEF4", hash_generated_field = "57967287B1D470C55211CDC70D6E48BD")

    private static final long serialVersionUID = 5034388446362600923L;

    /**
     * Constructs a new {@code NoSuchMethodException} that includes the current
     * stack trace.
     */
    @DSComment("From safe class list")
    @DSSafe(DSCat.SAFE_LIST)
    @DSGenerator(tool_name = "Doppelganger", tool_version = "2.0", generated_on = "2013-12-30 12:56:31.844 -0500", hash_original_method = "60C13DDE4E49448CC01A624FC168F601", hash_generated_method = "8ACD77AEE5C958EDECC1853EB1AEF39C")
    
public NoSuchMethodException() {
    }

    /**
     * Constructs a new {@code NoSuchMethodException} with the current stack
     * trace and the specified detail message.
     *
     * @param detailMessage
     *            the detail message for this exception.
     */
    @DSComment("From safe class list")
    @DSSafe(DSCat.SAFE_LIST)
    @DSGenerator(tool_name = "Doppelganger", tool_version = "2.0", generated_on = "2013-12-30 12:56:31.846 -0500", hash_original_method = "C31CF201699BCB0031463CB0C33C59AC", hash_generated_method = "87927B0C457162FCC37B35B04DC40709")
    
public NoSuchMethodException(String detailMessage) {
        super(detailMessage);
    }
}
