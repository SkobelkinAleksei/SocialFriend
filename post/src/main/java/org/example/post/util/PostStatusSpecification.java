package org.example.post.util;

import org.example.post.entity.PostEntity;
import org.example.post.entity.StatusPost;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class PostStatusSpecification {

    public static Specification<PostEntity> filter(List<StatusPost> status) {
        return (root, query, cb) -> {

            if (status == null || status.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("statusPost").in(status);
        };
    }

    public static Specification<PostEntity> byAuthor(Long authorId) {
        return (root, query, cb) -> cb.equal(root.get("authorId"), authorId);
    }
}
