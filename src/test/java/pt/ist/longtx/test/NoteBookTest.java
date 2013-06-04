package pt.ist.longtx.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.longtx.LogEntry;
import pt.ist.fenixframework.longtx.LongTransaction;
import pt.ist.fenixframework.longtx.TransactionalContext;

@RunWith(JUnit4.class)
public class NoteBookTest {

    private static final Logger logger = LoggerFactory.getLogger(NoteBookTest.class);

    private static User joao;
    private static User catarina;
    private static TransactionalContext context;
    private static NoteBook notebook;

    @BeforeClass
    public static void setup() {
        setupUsers();
        setupContext();
    }

    @Atomic
    public static void setupUsers() {
        DomainRoot domainRoot = FenixFramework.getDomainRoot();

        notebook = new NoteBook("Thesis Notes");

        joao = new User("Joao");
        joao.addNotebook(notebook);
        joao.addTransaction(context);

        catarina = new User("Catarina");
        catarina.addNotebook(new NoteBook("Networking Notes"));

        domainRoot.addUser(joao);
        domainRoot.addUser(catarina);
    }

    @Atomic
    public static void setupContext() {
        context = new TransactionalContext("thesis");
    }

    @Test
    public void testLongTransactionIsolation() {
        try {
            LongTransaction.setContextForThread(context);
            createNotes(joao, notebook);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        printNotes(joao, true);

        try {
            LongTransaction.setContextForThread(context);
            printNotes(joao, false);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        printNotes(joao, true);

        dumpContext();
    }

    @Atomic(mode = TxMode.READ)
    protected void dumpContext() {
        logger.info("Printing contents of context.");
        {
            logger.info("Write Set:");
            LogEntry entry = context.getWriteSet();
            while (entry != null) {
                logger.info("\t{}", entry.print());
                entry = entry.getNextEntry();
            }
        }
        {
            logger.info("Read Set:");
            LogEntry entry = context.getReadSet();
            while (entry != null) {
                logger.info("\t{}", entry.print());
                entry = entry.getNextEntry();
            }
        }
    }

    @Atomic(mode = TxMode.READ)
    private void printNotes(User user, boolean shouldBeEmpty) {
        logger.info("Printing notes for user {}", user.getName());
        for (NoteBook notebook : user.getNotebookSet()) {
            logger.info("Notebook: {}", notebook.getName());
            for (Note note : notebook.getNoteSet()) {
                logger.info("\tNote: {}", note.getContents());
            }
        }
        if (shouldBeEmpty) {
            assertThat(notebook.getNoteSet(), is(empty()));
        } else {
            assertThat(notebook.getNoteSet(), hasSize(10));
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private void createNotes(User user, NoteBook notes) {
        for (int i = 1; i <= 10; i++) {
            notes.addNote(new Note("Write chapter " + i));
        }
    }
}
