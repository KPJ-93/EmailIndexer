package nl.andrewl.email_indexer.data.export.query;

import nl.andrewl.email_indexer.data.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports query results to one or multiple plain-text files in a specific
 * directory.
 */
public final class PlainTextQueryExporter extends QueryExporter {
    private static final String MAIN_OUTPUT_FILE = "output.txt";

    /**
     * A print writer for the main text file in the export.
     */
    private PrintWriter printWriter;

    /**
     * The directory to generate the export in.
     */
    private Path outputDir;

    public PlainTextQueryExporter(QueryExportParams params) {
        super(params);
    }

    @Override
    protected void beforeExport(EmailDataset ds, Path path) throws IOException {
        outputDir = path;
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        printWriter = new PrintWriter(Files.newBufferedWriter(outputDir.resolve(MAIN_OUTPUT_FILE)), false);
        writeMetadata(ds);
    }

    @Override
    protected void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo) throws IOException {
        if (params.isSeparateEmailThreads()) {
            writeThreadInSeparateDocument(email, rank, emailRepo, tagRepo);
        } else {
            writeThreadInDocument(email, emailRepo, tagRepo, printWriter, 0);
        }
    }

    @Override
    protected void afterExport() {
        printWriter.close();
    }

    private void writeMetadata(EmailDataset ds) {
        printWriter.println("""
                Query: %s
                Exported at: %s
                Tags: %s,
                Total emails: %d""".formatted(
                params.getQuery(),
                ZonedDateTime.now().toString(),
                new TagRepository(ds).findAll().stream().map(Tag::name).collect(Collectors.joining(", ")),
                params.getResultCount()
        ));
        printWriter.println();
    }

    /**
     * Creates a new document and writes the mailing thread in it.
     */
    private void writeThreadInSeparateDocument(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo) throws IOException {
        try (PrintWriter p = new PrintWriter(Files.newBufferedWriter(outputDir.resolve("email-" + rank + ".txt")), false)) {
            writeThreadInDocument(email, emailRepo, tagRepo, p, 0);
        }
    }

    /**
     * Writes all information of a mailing thread into a plain-text document.
     */
    private void writeThreadInDocument(EmailEntry email, EmailRepository emailRepo, TagRepository tagRepo, PrintWriter p, int indentLevel) {
        String indent = "\t".repeat(indentLevel);
        p.println(indent + "Message id: " + email.messageId());
        p.println(indent + "Subject: " + email.subject());
        p.println(indent + "Sent from: " + email.sentFrom());
        p.println(indent + "Date: " + email.date());
        p.println(indent + "Tags: "
                + tagRepo.getTags(email.id()).stream().map(Tag::name).collect(Collectors.joining(", ")));
        p.println(indent + "Hidden: " + email.hidden());
        p.println(indent + "Body---->>>");
        email.body().trim().lines().forEachOrdered(line -> p.println(indent + line));
        p.println(indent + "-------->>>");
        List<EmailEntryPreview> replies = emailRepo.findAllReplies(email.id());
        if (!replies.isEmpty()) {
            p.println("Replies:");
            for (int i = 0; i < replies.size(); i++) {
                var reply = replies.get(i);
                EmailEntry replyFull = emailRepo.findEmailById(reply.id()).orElseThrow();
                p.println("\t" + indent + "Reply #" + (i + 1));
                writeThreadInDocument(replyFull, emailRepo, tagRepo, p, indentLevel + 1);
                p.println();
            }
        }
        p.println();
    }
}