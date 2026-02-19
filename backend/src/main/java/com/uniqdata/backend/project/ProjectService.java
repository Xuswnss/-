package com.uniqdata.backend.project;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<Project> findAll(Project.ProjectStatus status) {
        if (status == null) {
            return projectRepository.findAll();
        }
        return projectRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }

    @Transactional
    public Project create(ProjectCreateDto dto) {
        Project project = Project.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(Project.ProjectStatus.DRAFT)
                .escrowAmountXrp(dto.getEscrowAmountXrp() != null ? dto.getEscrowAmountXrp() : 0L)
                .build();
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, ProjectUpdateDto dto) {
        Project project = findById(id);
        if (dto.getTitle() != null) project.setTitle(dto.getTitle());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getStatus() != null) project.setStatus(dto.getStatus());
        if (dto.getEscrowAmountXrp() != null) project.setEscrowAmountXrp(dto.getEscrowAmountXrp());
        return projectRepository.save(project);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.deleteById(id);
    }
}
