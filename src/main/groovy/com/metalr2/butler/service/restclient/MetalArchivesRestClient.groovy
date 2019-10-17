package com.metalr2.butler.service.restclient

import com.metalr2.butler.service.parser.ReleaseDtoParser
import com.metalr2.butler.web.dto.ReleaseDto
import com.metalr2.butler.web.dto.UpcomingReleasesResponseDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class MetalArchivesRestClient {

  static final String UPCOMING_RELEASES_URL = "https://www.metal-archives.com/release/ajax-upcoming/json/1?sEcho=1&iDisplayStart={startOfRange}"

  final Logger LOG = LoggerFactory.getLogger(MetalArchivesRestClient)

  final RestTemplate restTemplate
  final ReleaseDtoParser parser
  final List<String[]> rawResponse = []

  @Autowired
  MetalArchivesRestClient(RestTemplate restTemplate, ReleaseDtoParser parser) {
    this.restTemplate = restTemplate
    this.parser = parser
  }

  List<ReleaseDto> requestReleases() {
    /*
     * The REST-interface of metal-archives.com responses a maximum of 100 records per request.
     * Therefore we have to request 100 records each until we don't get any more results.
     */
    def dataAvailable = true
    def startOfRange = 0

    while (dataAvailable) {
      // (1) request
      ResponseEntity<UpcomingReleasesResponseDto> responseEntity = restTemplate.getForEntity(UPCOMING_RELEASES_URL
              , UpcomingReleasesResponseDto.class
              , startOfRange)

      // (2) check http status and response body
      UpcomingReleasesResponseDto responseBody = responseEntity.body
      if (responseEntity.statusCode != HttpStatus.OK || responseBody == null) {
        break
      }

      // (3) collect raw response data
      rawResponse.addAll(responseBody.data)

      // (4) prepare next iteration
      if ((startOfRange + 100) < responseBody.totalRecords) {
        startOfRange += 100
      }
      else {
        dataAvailable = false
      }
    }

    List<ReleaseDto> results = []
    rawResponse.each { rawData ->
      try {
        results << parser.parse(rawData.toList())
      }
      catch (Exception e) { // ToDo DanielW: MayBe ParseException later
        LOG.error("Could not parse the following data: {}. Reason was: {}", rawData, e.getMessage())
      }
    }

    return results
  }

}
