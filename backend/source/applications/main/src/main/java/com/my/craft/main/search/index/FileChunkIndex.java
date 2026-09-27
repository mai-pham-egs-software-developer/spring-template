package com.my.craft.main.search.index;

import com.my.craft.opensearch.index.BaseIndexDocument;
import com.my.craft.opensearch.index.Document;
import com.my.craft.opensearch.index.Field;
import com.my.craft.opensearch.index.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One chunk of a file's content, keyed back to the file by {@code fileRefId}. The parent file's
 * {@code subject}/{@code documentType} are copied onto every chunk (denormalized, the way GitLab
 * copies parent data onto child documents instead of joining), so a query against this index
 * alone can collapse on {@code fileRefId} and still return the file's subject. When a file's
 * subject/documentType changes, every chunk with that {@code fileRefId} must be reindexed.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(index = "file-chunks")
public class FileChunkIndex extends BaseIndexDocument {

    /** The parent file's business key; every chunk of one file shares it. */
    @Field(type = FieldType.KEYWORD)
    private String fileRefId;

    @Field(type = FieldType.TEXT)
    private String subject;

    @Field(type = FieldType.KEYWORD)
    private String documentType;

    /** 0-based position of this chunk within the file, for re-assembling/ordering fragments. */
    @Field(type = FieldType.INTEGER)
    private Integer chunkNo;

    @Field(type = FieldType.TEXT)
    private String content;

    public static FileChunkIndex of(String fileRefId, String subject, String documentType, int chunkNo, String content) {
        FileChunkIndex chunk = new FileChunkIndex();
        chunk.setRefId(fileRefId + "-chunk-" + chunkNo);
        chunk.setFileRefId(fileRefId);
        chunk.setSubject(subject);
        chunk.setDocumentType(documentType);
        chunk.setChunkNo(chunkNo);
        chunk.setContent(content);
        return chunk;
    }
}
