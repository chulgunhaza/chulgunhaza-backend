package com.example.chulgunhazabackend.domain.board;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="posts")
@ToString
@Getter
@Builder
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    // #59: Post 작성자. 이전엔 Employee 엔티티가 아직 없어서 주석 처리돼 있었음
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Embedded
    private Category category;

    @Column(name = "title", columnDefinition = "VARCHAR(255)")
    private String title;

    @Column(name = "content", columnDefinition = "VARCHAR(1000)")
    private String content;

    @ToString.Exclude
    @ElementCollection
    @CollectionTable(name = "postFiles", joinColumns = @JoinColumn(name = "post_id"))
    private List<PostFile> postFilesList;

    @ColumnDefault("0")
    private int count;

    public Post(String title, String content, Category category, List<PostFile> postFilesList, Employee employee) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.postFilesList = postFilesList;
        this.employee = employee;
    }

    public void updatePost(String title, String content, Category category, List<PostFile> postFilesList) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.postFilesList = postFilesList;
    }

}
