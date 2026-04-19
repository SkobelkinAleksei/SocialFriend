package org.example.post.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.post.dto.NewPostDto;
import org.example.post.dto.PostDto;
import org.example.post.dto.UpdatePostDto;
import org.example.post.entity.StatusPost;
import org.example.post.entity.PostEntity;
import org.example.post.mapper.PostMapper;
import org.example.post.repository.PostRepository;
import org.example.post.util.PostAccessValidator;
import org.example.post.util.PostLookupService;
import org.example.post.util.PostStatusSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@AllArgsConstructor
@Service
public class PostServiceImpl implements PostService{

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostLookupService postLookupService;
    private final PostAccessValidator postAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public PostDto findPostById(Long postId) {
        log.info("[PostServiceImpl - INFO] Получение поста по id: {}", postId);
        PostEntity postEntity = postLookupService.getById(postId);

        log.info("[PostServiceImpl - INFO] Пост с id: {} успешно найден", postEntity.getId());
        return postMapper.toDto(postEntity);
    }

    @Override
    @Transactional
    public Long createPost(NewPostDto newPostDto, Long authorId) {
        log.info("[PostServiceImpl - INFO] Создание нового поста от автора с id: {}", authorId);

        PostEntity postEntity = postMapper.toEntity(newPostDto);
        postEntity.setAuthorId(authorId);
        postEntity.setStatusPost(StatusPost.PUBLISHED);
        postRepository.save(postEntity);

        log.info("[PostServiceImpl - INFO] Пост успешно создан, id: {}, автор id: {}", postEntity.getId(), authorId);
        return postEntity.getId();
    }

    @Override
    @Transactional
    public void updatePost(Long postId, UpdatePostDto updatePostDto, Long userId) {
        log.info("[PostServiceImpl - INFO] Обновление поста с id: {} пользователем с id: {}", postId, userId);

        PostEntity postEntity = postLookupService.getById(postId);
        postAccessValidator.validateAuthor(postEntity, userId);

        if (postEntity.getStatusPost().equals(StatusPost.REMOVED)) {
            log.warn("[PostServiceImpl - ERROR] Попытка изменить удалённый пост id: {}", postEntity.getId());
            throw new IllegalArgumentException("Нельзя изменить удаленный пост.");
        }

        postEntity.setContent(updatePostDto.getContent());
        log.info("[PostServiceImpl - INFO] Пост с id: {} успешно обновлён", postEntity.getId());
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        log.info("[PostServiceImpl - INFO] Удаление поста с id: {} пользователем с id: {}", postId, userId);

        PostEntity postEntity = postLookupService.getById(postId);
        postAccessValidator.validateAuthor(postEntity, userId);

        postEntity.setStatusPost(StatusPost.REMOVED);
        log.info("[PostServiceImpl - INFO] Пост с id: {} успешно удалён", postEntity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> findPostsByAuthor(Long authorId) {
        log.info("[PostServiceImpl - INFO] Получение опубликованных постов автора: {}", authorId);

        return postRepository.findAllByAuthorIdAndStatusPost(authorId, StatusPost.PUBLISHED)
                .stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> findUserPostsByStatus(
            Long authorId,
            List<StatusPost> status,
            int page,
            int size
    ) {
        //Это планируется, что автор постов сможет смотреть свои посты по статусам
        log.info("[PostServiceImpl - INFO] Поиск постов автора id: {} по статусам: {}, страница: {}, размер: {}",
                authorId, status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<PostEntity> spec = PostStatusSpecification
                .filter(status)
                .and(PostStatusSpecification.byAuthor(authorId));

        Page<PostEntity> postEntities = postRepository.findAll(spec, pageable);

        List<PostDto> postDtoList = postEntities.map(postMapper::toDto).toList();
        log.info("[PostServiceImpl - INFO] Поиск постов по статусам завершён. Найдено записей: {}", postDtoList.size());
        return postDtoList;
    }
}