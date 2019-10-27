package com.metalr2.butler.service

import com.metalr2.butler.model.release.ReleaseEntity
import com.metalr2.butler.model.release.ReleaseRepository
import com.metalr2.butler.service.converter.Converter
import com.metalr2.butler.web.dto.ReleaseDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate

@Service
class ReleaseServiceImpl implements ReleaseService {

  static final YESTERDAY = LocalDate.now() - 1

  final Logger log = LoggerFactory.getLogger(ReleaseServiceImpl)
  final ReleaseRepository releaseRepository
  final Converter<String[], List<ReleaseEntity>> converter

  @Autowired
  ReleaseServiceImpl(ReleaseRepository releaseRepository, Converter<String[], List<ReleaseEntity>> converter) {
    this.releaseRepository = releaseRepository
    this.converter = converter
  }

  @Override
  @Transactional
  void saveAll(List<String[]> upcomingReleasesRawData) {
    // convert raw string data into ReleaseEntity
    List<ReleaseEntity> releaseEntities = []
    upcomingReleasesRawData.each { releaseEntities.addAll(converter.convert(it)) }

    // remove any record that has a release date today or later
    def affectedRows = releaseRepository.deleteByReleaseDateIsAfter(YESTERDAY)
    log.info("{} records were deleted", affectedRows)

    // insert releases
    def inserted = releaseRepository.saveAll(releaseEntities.unique())
    log.info("{} records were inserted", inserted.size())
  }

  @Override
  @Transactional(readOnly = true)
  List<ReleaseDto> findAllUpcomingReleases(int page, int size) {
    Pageable pageable = PageRequest.of(page - 1, size) // page is index-based
    return releaseRepository.findAllByReleaseDateIsAfter(YESTERDAY, pageable)
                            .sort()
                            .collect { convertToDto(it) }
  }

  @Override
  @Transactional(readOnly = true)
  List<ReleaseDto> findAllReleasesInTimeRange(LocalDate from, LocalDate to, int page, int size) {
    Pageable pageable = PageRequest.of(page - 1, size) // page is index-based
    return releaseRepository.findAllByReleaseDateIsBetween(from, to, pageable)
                            .sort()
                            .collect { convertToDto(it) }
  }

  @Override
  @Transactional(readOnly = true)
  List<ReleaseDto> findAllUpcomingReleasesForArtists(List<String> artistNames, int page, int size) {
    // ToDo DanielW: implement
    return []
  }

  @Override
  @Transactional(readOnly = true)
  List<ReleaseDto> findAllReleasesInTimeRangeForArtists(List<String> artistNames, LocalDate from, LocalDate to, int page, int size) {
    // ToDo DanielW: implement
    return []
  }

  @Override
  long totalCountAllUpcomingReleases() {
    return releaseRepository.countByReleaseDateIsAfter(YESTERDAY)
  }

  @Override
  long totalCountAllReleasesInTimeRange(LocalDate from, LocalDate to) {
    return releaseRepository.countByReleaseDateIsBetween(from, to)
  }

  @Override
  long totalCountAllUpcomingReleasesForArtists() {
    // ToDo DanielW: implement
    return 0
  }

  @Override
  long totalCountAllReleasesInTimeRangeForArtists() {
    // ToDo DanielW: implement
    return 0
  }

  private ReleaseDto convertToDto(ReleaseEntity releaseEntity) {
    return new ReleaseDto(artist: releaseEntity.artist, additionalArtists: releaseEntity.additionalArtists,
                          albumTitle: releaseEntity.albumTitle, releaseDate: releaseEntity.releaseDate,
                          estimatedReleaseDate: releaseEntity.estimatedReleaseDate)
  }

}