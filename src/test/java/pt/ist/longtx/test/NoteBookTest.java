package pt.ist.longtx.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
import pt.ist.fenixframework.longtx.LongTransaction;
import pt.ist.fenixframework.longtx.TransactionalContext;

@RunWith(JUnit4.class)
public class NoteBookTest {

    private static final Logger logger = LoggerFactory.getLogger(NoteBookTest.class);

    private static User joao;
    private static User catarina;
    private static TransactionalContext context;
    private static NoteBook notebook;

    @Atomic
    @BeforeClass
    public static void setupUsers() {
        DomainRoot domainRoot = FenixFramework.getDomainRoot();

        context = new TransactionalContext("thesis");
        notebook = new NoteBook("Thesis Notes");

        joao = new User("Joao");
        joao.addNotebook(notebook);
        joao.addTransaction(context);

        catarina = new User("Catarina");
        catarina.addNotebook(new NoteBook("Networking Notes"));

        domainRoot.addUser(joao);
        domainRoot.addUser(catarina);
    }

    @Test
    public void testLongTransactionIsolation() {
        try {
            LongTransaction.setContextForThread(context);
            createNotes(joao, notebook);
        } finally {
            LongTransaction.removeContextFromThread();
        }

        printNotes(joao);
    }

    @Atomic(mode = TxMode.READ)
    private void printNotes(User user) {
        logger.info("Printing notes for user {}", user.getName());
        for (NoteBook notebook : user.getNotebookSet()) {
            logger.info("Notebook: {}", notebook.getName());
            for (Note note : notebook.getNoteSet()) {
                logger.info("\tNote: {}", note.getContents());
            }
        }
        assertThat(notebook.getNoteSet(), is(empty()));
    }

    @Atomic(mode = TxMode.WRITE)
    private void createNotes(User user, NoteBook notes) {
        for (int i = 1; i <= 10; i++) {
            notes.addNote(new Note("Write chapter " + i));
        }
    }
}
