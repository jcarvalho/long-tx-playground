package pt.ist.longtx.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

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
import pt.ist.fenixframework.core.TransactionError;
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

        printNotes(joao, 0);

        try {
            LongTransaction.setContextForThread(context);
            printNotes(joao, 10);
            createNotes(joao, notebook);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        printNotes(joao, 0);

        try {
            LongTransaction.setContextForThread(context);
            printNotes(joao, 20);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        printNotes(joao, 0);

        commitContext();

        printNotes(joao, 20);
    }

    @Test(expected = TransactionError.class)
    public void testConflictDetection() {
        setupContext();

        try {
            LongTransaction.setContextForThread(context);
            createNotes(joao, notebook);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        createNotes(joao, notebook);
        printNotes(joao, 30);

        try {
            LongTransaction.setContextForThread(context);
            printNotes(joao, 30);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        commitContext();
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateCommits() {
        setupContext();

        commitContext();
        commitContext();
    }

    @Atomic(mode = TxMode.WRITE)
    protected void commitContext() {
        context.commit(false);
    }

    @Atomic(mode = TxMode.READ)
    private void printNotes(User user, int size) {
        logger.info("Printing notes for user {}", user.getName());
        for (NoteBook notebook : user.getNotebookSet()) {
            logger.info("Notebook: {}", notebook.getName());
            for (Note note : notebook.getNoteSet()) {
                logger.info("\tNote: {}", note.getContents());
            }
        }
        assertThat(notebook.getNoteSet(), hasSize(size));
    }

    private static int noteCounter = 1;

    @Atomic(mode = TxMode.WRITE)
    private void createNotes(User user, NoteBook notes) {
        for (int i = 1; i <= 10; i++) {
            notes.addNote(new Note("Write chapter " + noteCounter++));
        }
    }
}
