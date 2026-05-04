package com.example.demo;

public class TagDTO {
    private Long id;
    private String tagName;
    private String tagColor;
    private Integer sortOrder;

    public TagDTO() {}

    public TagDTO(TaskTag tag) {
        this.id = tag.getId();
        this.tagName = tag.getTagName();
        this.tagColor = tag.getTagColor();
        this.sortOrder = tag.getSortOrder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public String getTagColor() { return tagColor; }
    public void setTagColor(String tagColor) { this.tagColor = tagColor; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}