/*
 * Copyright 2013, The Sporting Exchange Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.betfair.cougar.testing;

import com.betfair.cougar.logging.CougarLogger;
import com.betfair.cougar.logging.CougarLoggingUtils;

import java.util.Date;

public class EndDateTimeLogEntryCondition extends DateTimeLogEntryCondition
{
	final static CougarLogger logger = CougarLoggingUtils
	.getLogger(EndDateTimeLogEntryCondition.class);

	public EndDateTimeLogEntryCondition(String dateTimeFormatString)
	{
		super(dateTimeFormatString);
	}
	
	@Override
	// override this in specific conditions
	public boolean matchesEntry(String logEntry) {
		boolean matches = false;
		Date logDate = super.getDateFromLogEntry(logEntry);
		if((logDate != null) && (logDate.before(super.getCheckDate())))
		{
			matches = true;
		}
		return matches;
	}
	
}
