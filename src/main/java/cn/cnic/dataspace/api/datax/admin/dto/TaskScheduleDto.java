package cn.cnic.dataspace.api.datax.admin.dto;

public class TaskScheduleDto {

    /**
     * Selected job type:
     */
    Integer jobType;

    /**
     * What are the days of the week
     */
    Integer[] dayOfWeeks;

    /**
     * What day of the month
     */
    Integer[] dayOfMonths;

    /**
     * Seconds
     */
    Integer second;

    /**
     * Minute
     */
    Integer minute;

    /**
     * Hour
     */
    Integer hour;

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public Integer[] getDayOfWeeks() {
        return dayOfWeeks;
    }

    public void setDayOfWeeks(Integer[] dayOfWeeks) {
        this.dayOfWeeks = dayOfWeeks;
    }

    public Integer[] getDayOfMonths() {
        return dayOfMonths;
    }

    public void setDayOfMonths(Integer[] dayOfMonths) {
        this.dayOfMonths = dayOfMonths;
    }

    public Integer getSecond() {
        return second;
    }

    public void setSecond(Integer second) {
        this.second = second;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }
}
