package com.wicio.shiplog.route;

import com.wicio.shiplog.log.domain.Degree;
import com.wicio.shiplog.route.util.RandomNumberGenerator;
import com.wicio.shiplog.route.util.vo.DegreeRangeVO;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectionGeneratorBasedOnDirectionMinutesAgo {

  private final RandomNumberGenerator randomNumberGenerator;

  public Degree generateDirection(
      Instant currentTimeStamp,
      Integer lastDirection,
      Instant lastTimeStamp,
      long minutesAgo) {
    int returnValue;
    if (lastDirection == null) {
      returnValue = randomNumberGenerator.randomIntBetween(0, 360);
    } else {
      DegreeRangeVO range = rangeForImposedCourseRange(minutesAgo, lastDirection);
      returnValue = randomNumberGenerator.randomIntBetween(range.min()
          .getValue(), range.max()
          .getValue());
    }
    return letsMakeCogOnlyOnTheLeftSide(new Degree(returnValue));
  }

  /**
   * To prevent ships from going in circles -> go left
   */
  private Degree letsMakeCogOnlyOnTheLeftSide(Degree courseOverGround) {
    int cog = courseOverGround.getValue();
    if (cog > 180) {
      cog = cog - 180;
    } else {
      cog = cog + 180;
    }
    return new Degree(cog);
  }

  private DegreeRangeVO rangeForImposedCourseRange(long minutesPassed,
                                                   int lastDirection) {
    int possibleTurnValue = possibleVesselTurnValue(minutesPassed);

    return new DegreeRangeVO(
        new Degree(lastDirection - possibleTurnValue),
        new Degree(lastDirection + possibleTurnValue));
  }

  // arbitrary method of calculating turn value over time
  private int possibleVesselTurnValue(long minutesPassed) {
    return (int) (minutesPassed / 2);
  }

//  /**
//   * range is not between 0 and 360. Meaning values can be smaller than 0 and bigger than 360.
//   */
//  private Double[] directionRangeForTimeDifference(Double lastDirection,
//                                                   long minutes) {
//    Double radius = (double) minutes;
//    return new Double[]{lastDirection - radius, lastDirection + radius};
//  }
}
