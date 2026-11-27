package cloud.picon.backend.domain.favorite.controller;

import cloud.picon.backend.domain.favorite.dto.MyFavoriteResponse;
import cloud.picon.backend.domain.favorite.service.MyFavoriteService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.jwt.LoginMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me/favorites")
@RequiredArgsConstructor
public class MyFavoriteController implements MyFavoriteControllerDocs {

    private final MyFavoriteService service;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MyFavoriteResponse>>> find(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @LoginMemberId Long memberId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(service.find(memberId, page, size)));
    }
}
