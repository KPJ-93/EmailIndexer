package nl.andrewl.email_indexer.data.export.datasample.datatype;

import nl.andrewl.email_indexer.data.*;
import nl.andrewl.email_indexer.data.export.ExporterParameters;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * Query exporter that exports the results of email threads to a CSV file
 * that provides a quick overview of the information in each thread.
 */
public class CsvExporter implements TypeExporter {
	private static final String[] HEADERS = {
			"RANK",
			"ID",
			"MESSAGE_ID",
			"SUBJECT",
			"DATE",
			"TAGS",
			"REPLY_TAGS",
			"REPLY_COUNT",
			"BODY"
	};

	private CSVPrinter printer;

	@Override
	public void beforeExport(EmailDataset ds, Path path, ExporterParameters params) throws IOException {
		PrintWriter printWriter = new PrintWriter(path.toFile());
		CSVFormat format = CSVFormat.Builder.create(CSVFormat.RFC4180)
				.setHeader(HEADERS)
				.build();
		printer = new CSVPrinter(printWriter, format);
	}

	@Override
	public void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo) throws IOException {
		printer.printRecord(
				rank,
				email.id(),
				email.messageId(),
				email.subject(),
				email.date(),
				tagRepo.getTags(email.id()),
				tagRepo.getAllChildTags(email.id()),
				emailRepo.countRepliesRecursive(email.id()),
				email.body());
	}

	@Override
	public void afterExport() throws IOException {
		printer.close();
	}
}
