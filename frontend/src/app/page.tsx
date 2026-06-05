import Link from "next/link";

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center gap-6 p-8 text-center">
      <h1 className="text-3xl font-bold">commerce</h1>
      <p className="text-gray-500">패션 커머스 (포트폴리오)</p>
      <Link
        href="/products"
        className="rounded-full bg-gray-900 px-6 py-3 text-white transition hover:bg-gray-700"
      >
        상품 보러가기 →
      </Link>
    </main>
  );
}
