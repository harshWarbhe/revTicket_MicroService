package com.revticket.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "genre")
    private List<String> genre = new ArrayList<>();

    @Column(nullable = false)
    private Integer duration; // in minutes

    private String director;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_crew", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "crew_member")
    private List<String> crew = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    private String language;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Showtime> showtimes = new ArrayList<>();
}

