package org.unitime.timetable.util;

import java.util.Calendar;
import java.util.Date;

import org.unitime.timetable.ApplicationProperties;

public class FailedLoginAttempt {
	private final static int DEFAULT_LOCKOUT_MINUTES = 15;

	private String userId;
	private int count;
	private Date lastFailedAttempt;
	
	public FailedLoginAttempt (String userId, int count, Date lastFailedAttempt) {
		this.userId = userId;
		this.count = count;
		this.lastFailedAttempt = lastFailedAttempt;
	}		
			
	private int getNumberOfLockoutMinutes(){
		String numberOfLockoutMinutesStr = ApplicationProperties.getProperty("tmtbl.login.failed.lockout.minutes", Integer.toString(DEFAULT_LOCKOUT_MINUTES));
		int numberOfLockoutMinutes;
		try {
			numberOfLockoutMinutes = Integer.parseInt(numberOfLockoutMinutesStr);
		} catch (NumberFormatException e) {			
			numberOfLockoutMinutes = DEFAULT_LOCKOUT_MINUTES;
		}
		if (numberOfLockoutMinutes < 0){
			numberOfLockoutMinutes = DEFAULT_LOCKOUT_MINUTES;
		}
		return(numberOfLockoutMinutes);
	}
	
	public boolean isUserLockedOut(String user, Date attemptTime){
		if (userId != null && user != null && userId.equals(user) && count >= LoginManager.getMaxFailedAttempts()){
			Calendar checkTime = Calendar.getInstance();
			checkTime.setTime(lastFailedAttempt);
			Calendar attempt = Calendar.getInstance();
			attempt.setTime(attemptTime);
			checkTime.add(Calendar.MINUTE, getNumberOfLockoutMinutes());
			boolean lockedOut = attempt.before(checkTime);
			if (!lockedOut){
				count = 0;
			}
			return(lockedOut);
		} else {
			return(false);
		}
	}
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public Date getLastFailedAttempt() {
		return lastFailedAttempt;
	}
	public void setLastFailedAttempt(Date lastFailedAttempt) {
		this.lastFailedAttempt = lastFailedAttempt;
	}

}
