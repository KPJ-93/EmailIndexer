package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.util.Async;
import nl.andrewl.email_indexer.util.Status;
import nl.andrewl.mboxparser.MBoxParser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Component that generates email datasets from a set of directories containing
 * mbox files.
 */
public class EmailDatasetGenerator {
	private final Status status;

	public EmailDatasetGenerator(Status status) {
		this.status = status;
	}

	public EmailDatasetGenerator() {
		this(Status.noOp());
	}

	/**
	 * Generates a new Email Dataset using mbox files from a given directory,
	 * and places the resulting dataset in the given target location.
	 * @param mboxFileDirs The directories to read mbox files from.
	 * @param dsDir The directory to save the dataset at.
	 * @return A completion stage that completes when the dataset is created.
	 */
	public CompletionStage<Void> generate(Collection<Path> mboxFileDirs, Path dsDir) {
		return Async.run(() -> {
			status.sendMessage("Starting dataset generation.");
			Files.createDirectories(dsDir);
			status.sendMessage("Created dataset directory: " + dsDir);
			DatabaseGenerator dbGen = new DatabaseGenerator(dsDir.resolve("database"));
			status.sendMessage("Initialized embedded database.");
			List<Path> mboxFiles = new ArrayList<>();
			for (var dir : mboxFileDirs) mboxFiles.addAll(findMboxFiles(dir));
			status.setTotalSteps(mboxFiles.size());
			status.sendMessage("Found %d files to parse.".formatted(mboxFiles.size()));
			MBoxParser parser = new MBoxParser(new SanitizingEmailHandler(dbGen));
			for (var file : mboxFiles) {
				status.sendMessage("Parsing file: " + file);
				parser.parse(file);
				status.incrementStepsDone();
			}
			dbGen.close();
			status.sendMessage("All emails added to the database.");

			EmailDataset dataset = new EmailDataset(dsDir);
			status.sendMessage("Generating index.");
			new EmailIndexGenerator().generateIndex(dataset);
			status.sendMessage("Dataset generation complete.");
		});
	}

	private List<Path> findMboxFiles(Path dir) throws IOException {
		List<Path> files = new ArrayList<>();
		Files.walkFileTree(dir, new SimpleFileVisitor<>(){
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (Files.isReadable(file) && file.getFileName().toString().toLowerCase().endsWith(".mbox")) {
					files.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return files;
	}
}
