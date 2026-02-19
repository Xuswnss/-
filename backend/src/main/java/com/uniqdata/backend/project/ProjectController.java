package com.uniqdata.backend.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Projects", description = "연구(프로젝트) CRUD API")
@RestController
@RequestMapping("/api/v2/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "연구 목록 조회", description = "status, limit, offset으로 필터·페이징")
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        Project.ProjectStatus statusEnum = null;
        if (status != null && !status.equalsIgnoreCase("all")) {
            try {
                statusEnum = Project.ProjectStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Project> projects = projectService.findAll(statusEnum);
        int total = projects.size();
        List<Project> paged = projects.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        return ResponseEntity.ok(Map.of(
                "projects", paged,
                "total", total,
                "kpi", Map.of(
                        "total_projects", total
                )
        ));
    }

    @Operation(summary = "연구 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @Operation(summary = "연구 생성", description = "DRAFT 상태로 생성")
    @PostMapping
    public ResponseEntity<Project> create(@RequestBody ProjectCreateDto dto) {
        return ResponseEntity.ok(projectService.create(dto));
    }

    @Operation(summary = "연구 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<Project> update(@PathVariable Long id, @RequestBody ProjectUpdateDto dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    @Operation(summary = "연구 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
