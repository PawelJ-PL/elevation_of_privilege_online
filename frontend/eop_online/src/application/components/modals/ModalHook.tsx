import React, { ComponentType, useState } from "react"

type RequiredProps = { isOpen: boolean; onClose: () => void }
type Props<T> = T & RequiredProps

export function useModal<T>(Modal: ComponentType<Props<T>>, props: T): [JSX.Element, () => void] {
    const [visible, setVisible] = useState(false)

    return [
        // eslint-disable-next-line react/jsx-key
        <Modal {...props} isOpen={visible} onClose={() => setVisible(false)} />,
        () => setVisible(true),
    ]
}
