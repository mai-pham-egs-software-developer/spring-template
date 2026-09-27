package com.my.craft.main.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.my.craft.main.search.FileChunkIndexService;
import com.my.craft.main.search.index.FileChunkIndex;
import com.my.craft.main.service.dto.FileSearchItem;
import com.my.craft.main.service.dto.PitPage;
import com.my.craft.main.service.dto.ScrollPage;
import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;

/**
 * App side of the file search: builds the opensearch library's paging requests and converts its
 * {@link PitResponse} to this app's DTOs, so controllers never depend on the search library.
 */
@Service
public class FileSearchAppService {

    private static final String TEST_FILE_PREFIX = "test-file-";
    private static final int BULK_BATCH_SIZE = 500;
    private static final List<String> SUBJECT_ADJECTIVES = List.of("Drill Press", "Lathe", "Band Saw", "Milling Machine", "Welding", "Compressor", "Forklift", "Conveyor");
    private static final List<String> SUBJECT_NOUNS = List.of("Operator", "Maintenance", "Safety", "Installation", "Service");
    private static final List<String> SUBJECT_KINDS = List.of("Manual", "Guide", "Handbook", "Checklist", "Procedure");
    private static final List<String> DOCUMENT_TYPES = List.of("PDF", "DOCX", "TXT");
    private static final List<String> SENTENCES = List.of(
            "Inspect the belt tension before each use.",
            "Wear safety glasses and hearing protection at all times.",
            "Lubricate the spindle bearings every quarter.",
            "Check the emergency stop button during the daily inspection.",
            "Replace worn blades according to the maintenance plan.",
            "Keep the work area clean and free of debris.",
            "Disconnect power before changing any accessory.",
            "Record every repair in the service log.",
            "Calibrate the pressure gauge once a year.",
            "Report unusual noise or vibration to the supervisor immediately.",
            "Store hazardous materials in the approved cabinet.",
            "Only trained staff may operate the machine unattended.");

    private final FileChunkIndexService fileChunkIndexService;

    public FileSearchAppService(FileChunkIndexService fileChunkIndexService) {
        this.fileChunkIndexService = fileChunkIndexService;
    }

    public void createIndex() {
        fileChunkIndexService.registerMapping();
    }

    /** One sample file split into 3 chunks, subject/documentType copied onto each chunk. */
    public void seedSampleData() {
        String subject = "Drill Press Operator Manual";
        fileChunkIndexService.bulkIngest(List.of(
                FileChunkIndex.of("file-1", subject, "PDF", 0,
                        "Chapter 1: Introduction. This manual covers safe operation of the workshop drill press."),
                FileChunkIndex.of("file-1", subject, "PDF", 1,
                        "Chapter 2: Before each use, inspect the drill press chuck and check the belt tension."),
                FileChunkIndex.of("file-1", subject, "PDF", 2,
                        "Chapter 3: Maintenance schedule for the drill press is quarterly, per the maintenance plan.")));
    }

    /**
     * Test data: {@code files} files ({@code test-file-001}, ...), each split into a random 1..{@code
     * maxChunks} chunks of random sentences, ingested in batches of {@link #BULK_BATCH_SIZE}. The
     * same {@code seed} always produces the same data, and ids are stable, so re-seeding overwrites.
     * Returns the number of chunks ingested.
     */
    public int seedRandomData(int files, int maxChunks, long seed) {
        if (files < 1 || files > 10_000 || maxChunks < 1 || maxChunks > 1_000) {
            throw new IllegalArgumentException("files must be 1..10000 and maxChunks 1..1000");
        }
        fileChunkIndexService.deleteFilesByPrefix(TEST_FILE_PREFIX);
        Random random = new Random(seed);
        List<FileChunkIndex> chunks = new ArrayList<>();
        for (int f = 1; f <= files; f++) {
            String fileRefId = TEST_FILE_PREFIX + "%03d".formatted(f);
            String subject = pick(random, SUBJECT_ADJECTIVES) + " " + pick(random, SUBJECT_NOUNS) + " " + pick(random, SUBJECT_KINDS);
            String documentType = pick(random, DOCUMENT_TYPES);
            int chunkCount = 1 + random.nextInt(maxChunks);
            for (int c = 0; c < chunkCount; c++) {
                chunks.add(FileChunkIndex.of(fileRefId, subject, documentType, c, randomContent(random, c)));
            }
        }
        for (int from = 0; from < chunks.size(); from += BULK_BATCH_SIZE) {
            fileChunkIndexService.bulkIngest(chunks.subList(from, Math.min(from + BULK_BATCH_SIZE, chunks.size())));
        }
        return chunks.size();
    }

    private static String randomContent(Random random, int chunkNo) {
        StringBuilder content = new StringBuilder("Chapter ").append(chunkNo + 1).append(':');
        List<String> sentences = new ArrayList<>(SENTENCES);
        Collections.shuffle(sentences, random);
        sentences.subList(0, 2 + random.nextInt(5)).forEach(s -> content.append(' ').append(s));
        return content.toString();
    }

    private static String pick(Random random, List<String> values) {
        return values.get(random.nextInt(values.size()));
    }

    /** One item per file. */
    public PitPage<FileSearchItem> search(String keyword, String pitId, int size, long ttl) {
        PitResponse<FileSearchItem> response = fileChunkIndexService.searchFiles(keyword, new PitRequest(pitId, size, ttl));
        return new PitPage<>(response.data(), response.pitId(), response.remainingDuration());
    }

    /** One item per file with all its matching chunks as snippets; {@code size} counts files. */
    public ScrollPage<FileSearchItem> scroll(String keyword, String scrollId, int size, long ttl) {
        PitResponse<FileSearchItem> response =
                fileChunkIndexService.scrollFiles(keyword, new PitRequest(scrollId, size, ttl));
        return new ScrollPage<>(response.data(), response.pitId(), response.remainingDuration());
    }
}
