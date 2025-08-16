package com.planty.repository.board;

import com.planty.dto.board.BoardAllResDto;
import com.planty.entity.board.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


// 판매 게시판 레포지토리
public interface BoardRepository extends JpaRepository<Board, Integer> {
    // 판매 게시글 상세 정보 가져오기
    @EntityGraph(attributePaths = {"user","crop","images"})
    Optional<Board> findById(Integer id);

    // 판매 게시글 전체 목록 가져오기
    List<Board> findAllByOrderByCreatedAtDesc();

    // 제목, 카테고리로 판매 게시글 검색하기
    @Query("""
        select distinct b
        from Board b
        left join fetch b.images i
        where b.title like :pattern
           or exists (
                select 1 from CropCategory cc
                where cc.crop = b.crop
                  and cc.categoryName like :pattern
           )
        order by b.createdAt desc
    """)
    List<Board> searchByKeyword(@Param("pattern") String pattern);

    // 게시글 기준으로 crop 아이디 가져오기
    @Query("select b.crop.id from Board b where b.id = :boardId")
    Integer findCropIdByBoardId(@Param("boardId") Integer boardId);

    // 내가 쓴 판매 게시글 불러오기
    @Query("""
        select b
        from Board b
        left join fetch b.images
        where b.user.id = :userId
        order by b.sell asc, b.createdAt desc
    """)
    List<Board> findMyBoardsOrderByStatusAndCreated(@Param("userId") Integer userId);

}
