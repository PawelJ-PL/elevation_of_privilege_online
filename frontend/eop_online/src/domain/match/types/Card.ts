export type Card = {
    cardNumber: number,
    value: string,
    suit: Suit,
    text: string
}

export type Suit = "Spoofing" | "Tampering" | "Repudiation" | "InformationDisclosure" | "DenialOfService" | "ElevationOfPrivilege"