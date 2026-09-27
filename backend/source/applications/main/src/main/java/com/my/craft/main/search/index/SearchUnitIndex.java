package com.my.craft.main.search.index;

import com.my.craft.main.service.dto.SearchType;
import com.my.craft.opensearch.index.BaseIndexDocument;
import com.my.craft.opensearch.index.Document;
import com.my.craft.opensearch.index.Field;
import com.my.craft.opensearch.index.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One searchable unit in a single shared index: a whole PLAN, or one chunk of a FILE. Every unit
 * carries its parent's display data ({@code title}, {@code documentType}) so one query can match
 * content, collapse on {@code parentRefId} and still return the parent's title without a join.
 * A PLAN is its own parent ({@code parentRefId == refId}) with {@code content} = its description.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(index = "search-units")
public class SearchUnitIndex extends BaseIndexDocument {

    @Field(type = FieldType.KEYWORD)
    private String parentRefId;

    @Field(type = FieldType.KEYWORD)
    private SearchType type;

    @Field(type = FieldType.TEXT)
    private String title;

    @Field(type = FieldType.KEYWORD)
    private String documentType;

    @Field(type = FieldType.INTEGER)
    private Integer chunkNo;

    @Field(type = FieldType.TEXT)
    private String content;

    public static SearchUnitIndex ofPlan(String refId, String title, String description) {
        SearchUnitIndex unit = new SearchUnitIndex();
        unit.setRefId(refId);
        unit.setParentRefId(refId);
        unit.setType(SearchType.PLAN);
        unit.setTitle(title);
        unit.setChunkNo(0);
        unit.setContent(description);
        return unit;
    }

    public static SearchUnitIndex ofFileChunk(
            String fileRefId, String subject, String documentType, int chunkNo, String content) {
        SearchUnitIndex unit = new SearchUnitIndex();
        unit.setRefId(fileRefId + "-chunk-" + chunkNo);
        unit.setParentRefId(fileRefId);
        unit.setType(SearchType.FILE);
        unit.setTitle(subject);
        unit.setDocumentType(documentType);
        unit.setChunkNo(chunkNo);
        unit.setContent(content);
        return unit;
    }
}
