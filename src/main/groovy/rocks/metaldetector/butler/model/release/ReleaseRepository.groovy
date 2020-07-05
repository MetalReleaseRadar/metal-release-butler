package rocks.metaldetector.butler.model.release

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

import java.time.LocalDate

@Repository
interface ReleaseRepository extends JpaRepository<ReleaseEntity, Long> {

  Page<ReleaseEntity> findAllByReleaseDateAfter(LocalDate date, Pageable pageable)

  Page<ReleaseEntity> findAllByReleaseDateBetween(LocalDate from, LocalDate to, Pageable pageable)

  Page<ReleaseEntity> findAllByReleaseDateAfterAndArtistIn(LocalDate date, Iterable<String> artistNames, Pageable pageable)

  Page<ReleaseEntity> findAllByArtistInAndReleaseDateBetween(Iterable<String> artistNames, LocalDate from, LocalDate to, Pageable pageable)

  List<ReleaseEntity> findAllByReleaseDateAfter(LocalDate date)

  List<ReleaseEntity> findAllByReleaseDateBetween(LocalDate from, LocalDate to)

  List<ReleaseEntity> findAllByReleaseDateAfterAndArtistIn(LocalDate date, Iterable<String> artistNames)

  List<ReleaseEntity> findAllByArtistInAndReleaseDateBetween(Iterable<String> artistNames, LocalDate from, LocalDate to)

  Optional<ReleaseEntity> findByArtistAndAlbumTitleAndReleaseDate(String artist, String albumTitle, LocalDate releaseDate)

  boolean existsByArtistAndAlbumTitleAndReleaseDate(String artist, String albumTitle, LocalDate releaseDate)

  long countByReleaseDateAfter(LocalDate date)

  long countByArtistInAndReleaseDateAfter(Iterable<String> artistNames, LocalDate date)

  long countByReleaseDateBetween(LocalDate from, LocalDate to)

  long countByArtistInAndReleaseDateBetween(Iterable<String> artistNames, LocalDate from, LocalDate to)

}