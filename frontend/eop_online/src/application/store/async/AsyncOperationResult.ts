export enum OperationStatus {
    NOT_STARTED = "NOT_STARTED",
    PENDING = "PENDING",
    FINISHED = "FINISHED",
    FAILED = "FAILED",
}

export default interface AsyncOperationsResult<Data, Error, Params> {
    status: OperationStatus
    data?: Data
    error?: Error
    params?: Params
}
