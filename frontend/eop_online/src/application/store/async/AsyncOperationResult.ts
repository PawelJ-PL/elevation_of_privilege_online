export enum OperationStatus {
    NOT_STARTED = "NOT_STARTED",
    PENDING = "PENDING",
    FINISHED = "FINISHED",
    FAILED = "FAILED",
}

// eslint-disable-next-line @typescript-eslint/consistent-type-definitions
export default interface AsyncOperationsResult<Data, Error, Params> {
    status: OperationStatus
    data?: Data
    error?: Error
    params?: Params
}
