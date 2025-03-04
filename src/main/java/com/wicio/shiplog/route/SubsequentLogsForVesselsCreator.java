package com.wicio.shiplog.route;

import com.wicio.shiplog.log.api.dto.CreateLogRequest;
import com.wicio.shiplog.log.domain.Degree;
import com.wicio.shiplog.log.domain.Log;
import com.wicio.shiplog.route.util.TimeDifferenceCalculator;
import com.wicio.shiplog.route.producer.NewVesselLogEvent;
import com.wicio.shiplog.vessel.domain.Vessel;
import com.wicio.shiplog.vessel.domain.VesselRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubsequentLogsForVesselsCreator {

  private final VesselRepository vesselRepository;
  private final TimeDifferenceCalculator timeDifferenceCalculator;
  private final DistanceCalculatorBasedOnSpeedAndTime distanceCalculatorBasedOnSpeedAndTime;
  private final DirectionGeneratorBasedOnDirectionMinutesAgo directionGeneratorBasedOnDirectionMinutesAgo;
  private final SpeedGenerator speedGenerator;
  private final NewCoordinatesGenerator newCoordinatesGenerator;

  private final SpeedGeneratorConfigDTO speedOverGroundConfigVO =
      new SpeedGeneratorConfigDTO(0, 20, 20);

  private final SpeedGeneratorConfigDTO windSpeedConfigVO =
      new SpeedGeneratorConfigDTO(0, 40, 40);

  List<NewVesselLogEvent>  execute() {
    List<Vessel> vessels = vesselRepository.findAllByLastLogIsNotNull();

    Instant currentTimeStamp;
    Instant lastTimeStamp;
    int speedOverGround;
    Degree courseOverGround;
    Degree windDirection;
    int windSpeed;

    ArrayList<NewVesselLogEvent> list = new ArrayList<>();

    for (Vessel vessel : vessels) {
      currentTimeStamp = Instant.now();
      Log lastLog = vessel.getLastLog();
      lastTimeStamp = lastLog.getCreatedAt();
      speedOverGround = speedGenerator.generateSpeedOverGround(
          speedOverGroundConfigVO,
          lastLog.getSpeedOverGroundInKmPerHour(),
          timeDifferenceCalculator.minutesBetween(currentTimeStamp, lastTimeStamp));
      courseOverGround =
          directionGeneratorBasedOnDirectionMinutesAgo.generateDirection(
              lastLog.getCourseOverGround(),
              timeDifferenceCalculator.minutesBetween(currentTimeStamp, lastTimeStamp));
      windDirection =
          directionGeneratorBasedOnDirectionMinutesAgo.generateDirection(
              lastLog.getWindDirection(),
              timeDifferenceCalculator.minutesBetween(currentTimeStamp, lastTimeStamp));
      windSpeed = speedGenerator.generateSpeedOverGround(
          windSpeedConfigVO,
          lastLog.getWindSpeedInKmPerHour(),
          timeDifferenceCalculator.minutesBetween(currentTimeStamp, lastTimeStamp));

      Point point = newCoordinatesGenerator.basedOnDistanceAndBearing(
          lastLog.getPoint()
              .getY(),
          lastLog.getPoint()
              .getX(),
          distanceCalculatorBasedOnSpeedAndTime.distanceInMetres(
              lastLog.getSpeedOverGroundInKmPerHour(),
              timeDifferenceCalculator.minutesBetween(currentTimeStamp,
                  lastTimeStamp)),
          courseOverGround.getValue()
      );

      list.add(new NewVesselLogEvent(
          vessel.getId(),
          CreateLogRequest.builder()
              .YCoordinate(point.getY())
              .XCoordinate(point.getX())
              .speedOverGround(speedOverGround)
              .courseOverGround(courseOverGround.getValue())
              .windDirection(windDirection.getValue())
              .windSpeed(windSpeed)
              .stationary(speedOverGround == 0)
              .build()
      ));
    }
    return list;
  }

}
