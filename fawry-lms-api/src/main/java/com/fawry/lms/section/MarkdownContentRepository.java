package com.fawry.lms.section;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.section.entities.MarkdownContent;

public interface MarkdownContentRepository extends JpaRepository<MarkdownContent, Long> {
}