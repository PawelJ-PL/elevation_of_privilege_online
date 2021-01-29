import { UseToastOptions } from "@chakra-ui/react"

export const toastMock = (callMock?: jest.Mock) => {
    const mock = callMock ?? jest.fn()

    const fn = (options: UseToastOptions) => {
        mock(options)
        return undefined
    }
    const rest = {
        close: () => void 0,
        closeAll: () => void 0,
        update: () => void 0,
        isActive: () => void 0,
    }

    return Object.assign(fn, rest)
}
