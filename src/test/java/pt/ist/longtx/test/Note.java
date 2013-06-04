package pt.ist.longtx.test;

import org.joda.time.DateTime;

public class Note extends Note_Base {

    public Note(String contents) {
        setContents(contents);
        setCreation(new DateTime());
    }

}
