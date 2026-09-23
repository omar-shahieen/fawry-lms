package com.fawry.lms.section;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.section.entities.MarkdownContent;
import com.fawry.lms.section.entities.Section;
import java.util.List;

public interface MarkdownContentRepository extends JpaRepository<MarkdownContent, Long> {
    List<MarkdownContent> findBySectionOrderByCreatedAtAsc(Section section);
}
