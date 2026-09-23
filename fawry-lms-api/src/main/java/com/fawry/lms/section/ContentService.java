package com.fawry.lms.section;

import com.fawry.lms.section.dto.ContentResponse;
import com.fawry.lms.section.dto.CreateContentRequest;
import com.fawry.lms.section.dto.UpdateContentRequest;
import com.fawry.lms.section.entities.MarkdownContent;
import com.fawry.lms.section.entities.Section;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContentService {

    private final MarkdownContentRepository contentRepository;
    private final SectionRepository sectionRepository;

    public ContentService(MarkdownContentRepository contentRepository, SectionRepository sectionRepository) {
        this.contentRepository = contentRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> list(Long sectionId) {
        Section section = findSection(sectionId);
        return contentRepository.findBySectionOrderByCreatedAtAsc(section).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContentResponse get(Long id) {
        return toResponse(findContent(id));
    }

    @Transactional
    public ContentResponse create(Long sectionId, CreateContentRequest request) {
        MarkdownContent content = new MarkdownContent();
        content.setSection(findSection(sectionId));
        content.setTitle(request.title());
        content.setBody(request.body());
        return toResponse(contentRepository.saveAndFlush(content));
    }

    @Transactional
    public ContentResponse update(Long id, UpdateContentRequest request) {
        MarkdownContent content = findContent(id);
        if (request.title() != null) content.setTitle(request.title());
        if (request.body() != null) content.setBody(request.body());
        return toResponse(contentRepository.saveAndFlush(content));
    }

    @Transactional
    public void delete(Long id) {
        contentRepository.delete(findContent(id));
    }

    @Transactional(readOnly = true)
    public com.fawry.lms.course.entities.Course getCourse(Long id) {
        return findContent(id).getSection().getCourse();
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Section not found."));
    }

    private MarkdownContent findContent(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found."));
    }

    private ContentResponse toResponse(MarkdownContent content) {
        return new ContentResponse(
                content.getId(),
                content.getSection().getId(),
                content.getTitle(),
                content.getBody(),
                content.getCreatedAt(),
                content.getUpdatedAt());
    }
}
