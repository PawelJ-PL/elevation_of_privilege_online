import { BoxProps, FormControl, FormErrorMessage, FormLabel, Input, InputProps } from "@chakra-ui/core"
import React from "react"

type OmitedInputProps = "id" | "onChange"

type Props = {
    containerProps?: BoxProps
    errorMessageProps?: BoxProps
    required?: boolean
    errorMessage?: string
    label?: string
    inputId?: string
    onTextChange?: (data: string) => void
} & Omit<InputProps, OmitedInputProps>

const FormInput: React.FC<Props> = ({
    containerProps,
    required,
    errorMessage,
    label,
    inputId,
    onTextChange,
    errorMessageProps,
}) => (
    <FormControl isRequired={required} isInvalid={Boolean(errorMessage)} {...(containerProps || {})}>
        {label && <FormLabel htmlFor={inputId}>{label}</FormLabel>}
        <Input
            id={inputId}
            onChange={onTextChange === undefined ? undefined : (event) => onTextChange(event.target.value)}
        />
        {Boolean(errorMessage) && <FormErrorMessage {...(errorMessageProps || {})}>{errorMessage}</FormErrorMessage>}
    </FormControl>
)

export default FormInput
