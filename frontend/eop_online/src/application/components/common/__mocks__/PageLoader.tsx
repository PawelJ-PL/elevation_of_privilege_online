import React from "react"

const PageLoader: React.FC<{ text: string }> = ({ text }) => <div data-testid="LOADER_MOCK">{text}</div>

export default PageLoader
