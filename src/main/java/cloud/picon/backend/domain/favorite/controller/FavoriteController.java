package cloud.picon.backend.domain.favorite.controller;

import cloud.picon.backend.domain.favorite.service.FavoriteService;
import cloud.picon.backend.global.jwt.LoginMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class FavoriteController implements FavoriteControllerDocs {

    private final FavoriteService favoriteService;

    @Override
    @PostMapping("/{expertId}/favorite")
    public ResponseEntity<Void> add(@PathVariable long expertId, @LoginMemberId Long memberId) {
        favoriteService.add(expertId, memberId);
        return ResponseEntity.noContent().build();
    }
}
