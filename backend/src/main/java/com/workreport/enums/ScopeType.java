package com.workreport.enums;

/**
 * Represents the data-access scope granted to the currently authenticated user.
 * Resolved by {@link com.workreport.aop.DataScopeAspect} at run-time based on the
 * user's {@link Role}.
 *
 * <ul>
 *   <li>{@code SELF}       – user may only access their own records (EXECUTOR)</li>
 *   <li>{@code DEPARTMENT} – user may access all records within their department (DEPT_MANAGER)</li>
 *   <li>{@code PROJECT}    – user may access all records within their managed projects (PM)</li>
 *   <li>{@code ALL}        – unrestricted access (ADMIN, HR)</li>
 * </ul>
 */
public enum ScopeType {
    SELF,
    DEPARTMENT,
    PROJECT,
    ALL
}
