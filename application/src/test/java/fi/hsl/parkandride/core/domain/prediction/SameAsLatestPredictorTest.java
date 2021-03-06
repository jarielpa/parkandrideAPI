// Copyright © 2015 HSL <https://www.hsl.fi>
// This program is dual-licensed under the EUPL v1.2 and AGPLv3 licenses.

package fi.hsl.parkandride.core.domain.prediction;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@Transactional
public class SameAsLatestPredictorTest extends AbstractPredictorTest {

    public SameAsLatestPredictorTest() {
        super(new SameAsLatestPredictor());
    }

    @Test
    public void when_no_history_then_no_predictions() {
        assertThat(predict(), is(empty()));
        assertThat(predictorState.latestUtilization, is(new DateTime(0)));
    }

    @Test
    public void when_some_history_then_repeats_the_latest_prediction_for_the_next_24h() {
        insertUtilization(now, 42);

        assertThat(predict(), is(Arrays.asList(
                new Prediction(now, 42),
                new Prediction(now.plusDays(1), 42))));
        assertThat(predictorState.latestUtilization, is(now));
    }
}
