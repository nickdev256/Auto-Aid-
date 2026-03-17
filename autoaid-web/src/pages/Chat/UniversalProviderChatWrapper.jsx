import React from "react";
import { useParams } from "react-router-dom";
import UniversalProviderChat from "./UniversalProviderChat";

export default function UniversalProviderChatWrapper() {
  const { requestId } = useParams();
  return <UniversalProviderChat requestId={requestId} />;
}
